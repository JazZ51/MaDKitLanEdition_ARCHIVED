/*
 * Copyright 1997-2012 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit.
 * 
 * MaDKit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * MaDKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import madkit.action.ActionInfo;
import madkit.action.GUIManagerAction;
import madkit.action.KernelAction;
import madkit.agr.LocalCommunity;
import madkit.agr.LocalCommunity.Groups;
import madkit.i18n.Words;
import madkit.kernel.AbstractAgent;
import madkit.kernel.Agent;
import madkit.kernel.Madkit.BooleanOption;
import madkit.kernel.Madkit.Option;
import madkit.kernel.Message;
import madkit.message.GUIMessage;
import madkit.message.KernelMessage;

/**
 * The GUI manager agent is responsible for setting and managing
 * agents UI which are created by the default mechanism of MaDKit.
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.6
 * @version 0.9
 * 
 */
// * By default the kernel always launch this agent. Although, this agent is
// * extremely light weight, it is possible to tell the kernel to not launch it
// * by using the {@link BooleanOption#noGUIManager} option when launching MaDKit.
class GUIManagerAgent extends Agent {

	private static final long									serialVersionUID	= 8026421822077510523L;
	final private ConcurrentMap<AbstractAgent, JFrame>	guis;
	private boolean												shuttedDown			= false;

	private JDesktopPane											desktopPane;

	private MDKDesktopFrame											myFrame;

	GUIManagerAgent(boolean asDaemon) { // NO_UCD use by reflection
		super(asDaemon);
		guis = new ConcurrentHashMap<AbstractAgent, JFrame>();
	}

	@Override
	protected void activate() {// TODO parallelize that
	// setLogLevel(Level.ALL);
	// requestRole(LocalCommunity.NAME, Groups.SYSTEM, Roles.GUI_MANAGER);//no need: I am a manager
		if (!isDaemon()) {// use to detect desktop mode
			try {
				buildUI();
				if(ActionInfo.javawsIsOn)
					setMadkitProperty(BooleanOption.autoConnectMadkitWebsite.name(), "true");
			} catch (HeadlessException e) {
				headlessLog(e);
				return;
			} catch (InstantiationException e) {
				buildUIExceptionLog(e);
			} catch (IllegalAccessException e) {
				buildUIExceptionLog(e);
			} catch (ClassNotFoundException e) {
				buildUIExceptionLog(e);
			}
		}
		createGroup(LocalCommunity.NAME, Groups.GUI);
	}

	/**
	 * @param e
	 */
	public void buildUIExceptionLog(Exception e) {
		getLogger().severeLog(Words.FAILED.toString()+ " : UI creation", e);
	}

	/**
	 * @param e
	 */
	private void headlessLog(HeadlessException e) {
		getLogger().severe(
				"\t" + e.getMessage() + "\n\tNo graphic environment, quitting");
		shuttedDown = true;
	}

	@Override
	protected void live() {
		while (! shuttedDown) {
			final Message m = waitNextMessage();
			if (m instanceof GUIMessage) {
				proceedCommandMessage((GUIMessage) m);
			}
			else
				if (m instanceof KernelMessage) {
					proceedEnumMessage((KernelMessage) m);
				}
				else
					if (logger != null)
						logger.warning("I received a message that I do not understand. Discarding "
								+ m);
		}
	}

	private void proceedCommandMessage(GUIMessage cm) {
		if (isAlive()) {
			if (cm.getCode() == GUIManagerAction.SETUP_AGENT_GUI) {// because it needs a reply
				try {
					setupAgentGui((AbstractAgent) cm.getContent()[0]);
					sendReply(cm, cm);
				} catch (HeadlessException e) {
					headlessLog(e);
				}
			}
			else {
				super.proceedEnumMessage(cm);
			}
		}
	}

	@Override
	protected void end() {
		if (logger != null)
			logger.finer("Ending: Disposing frames");
		// SwingUtilities.invokeLater(
		// new Runnable() {
		// public void run() {
		killAgents(); // no need because it closes internal frames too
		if (myFrame != null) {// TODO swing thread or cleaner shutdown
			myFrame.dispose();
		}
		// }});
	}

	@SuppressWarnings("unused")
	private void exit() {
		shuttedDown = true;
	}

	private void setupAgentGui(final AbstractAgent agent) {
		if (!shuttedDown && agent.isAlive()) {
			if (logger != null)
				logger.fine("Setting up GUI for " + agent);
			AgentFrame f = new AgentFrame(agent, agent.getName());
			try{
				agent.setupFrame(f);// TODO catch failures because of delegation
			} catch (Exception e) {
				agent.getLogger().severeLog(
						"Frame setup problem -> default GUI settings", e);
				f = new AgentFrame(agent, agent.getName());
			}
			guis.put(agent, f);
			final AgentFrame af = f;
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					if (desktopPane != null) {
						final JInternalFrame jf = buildInternalFrame(af);
						desktopPane.add(jf);
						jf.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
						jf.addInternalFrameListener(new InternalFrameAdapter() {
							@Override
							public void internalFrameClosing(InternalFrameEvent e) {
								if (agent.isAlive()) {
									jf.setTitle("Closing " + agent.getName());
									AgentFrame.killAgent(agent, 2);
								}
								else {
									jf.dispose();
								}
							}
						});
						jf.setLocation(checkLocation(jf));
						jf.setVisible(true);
					}
					else {
						af.setLocation(checkLocation(af));
						af.setVisible(true);
					}
				}
			});
		}
	}

	private JInternalFrame buildInternalFrame(final AgentFrame af) {
//		final JInternalFrame ijf = new JInternalFrame(af.getTitle(), true, true,
//				true, true);
		final JInternalFrame ijf = new JInternalFrame(af.getTitle(), true, true,
				true, true);
		ijf.setFrameIcon(new ImageIcon(af.getIconImage().getScaledInstance(14,
				14, java.awt.Image.SCALE_SMOOTH)));
		ijf.setSize(af.getSize());
		ijf.setLocation(af.getLocation());
//		ijf.setContentPane(af.getContentPane());
		ijf.setJMenuBar(af.getJMenuBar());
		af.setInternalFrame(ijf);
		return ijf;
	}

	private void iconifyAll(boolean iconify) {
		final int code = iconify ? JFrame.ICONIFIED : JFrame.NORMAL;
		for (final JFrame f : guis.values()) {
			f.setExtendedState(code);
		}
		if (myFrame != null) {//FIXME no need now, because this is only in desktop mode, but probably in the future
			for (JInternalFrame ijf : desktopPane.getAllFrames()) {
				try {
					ijf.setIcon(iconify);
				} catch (PropertyVetoException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void iconifyAll() {
		iconifyAll(true);
	}

	@SuppressWarnings("unused")
	private void deiconifyAll() {
		iconifyAll(false);
	}

	@SuppressWarnings("unused")
	private void disposeAgentGui(AbstractAgent agent) {// TODO event dispatch thread ?
		final JFrame f = guis.remove(agent);
		if (f != null) {
			f.dispose();
		}
	}

	private Point checkLocation(Container c) {
		Dimension dim;
		List<? extends Container> l;
		if (c instanceof JInternalFrame) {
			dim = desktopPane.getSize();
			l = Arrays.asList(desktopPane.getAllFrames());
		}
		else {
			dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			l = new ArrayList<Container>(guis.values());
		}
		// dim.setSize(dim.width, dim.height-25);
		Dimension size = c.getSize();
		if (size.width > dim.width)
			size.width = dim.width;
		if (size.height > dim.height)
			size.height = dim.height;
		c.setSize(size);
		dim.width -= 20;
		boolean notGood = true;
		Point location = c.getLocation();
		location.x = location.x > 0 ? location.x : 1;
		location.y = location.y > 0 ? location.y : 1;
		location.x = location.x <= dim.width ? location.x : location.x
				% dim.width;
		location.y = location.y <= dim.height ? location.y : location.y
				% dim.height;
		while (notGood) {
			notGood = false;
			for (Container cs : l) {
				if (cs != c && location.equals(cs.getLocation())) {
					notGood = true;
					location.x += 20;
					location.x %= dim.width;
					location.y += 20;
					location.y %= dim.height;
				}
			}
		}
		return location;
	}

	/**
	 * Kills all the agents that have a GUI
	 */
	private void killAgents() {
		for (final JFrame f : guis.values()) {
			f.dispose();
		}
		guis.clear();
	}

	private void buildUI() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		myFrame = (MDKDesktopFrame) getMadkitClassLoader().loadClass(getMadkitProperty(Option.desktopFrameClass.name())).newInstance();
		desktopPane = myFrame.getDesktopPane();
		// {
		// @Override
		// protected void paintComponent(Graphics g) {
		// super.paintComponent(g);
		// Graphics2D g2d = (Graphics2D) g;
		// int x = (this.getWidth() - image.getWidth(null)) / 2;
		// int y = (this.getHeight() - image.getHeight(null)) / 2;
		// g2d.drawImage(image, x, y, null);
		// }
		// }
		myFrame.setJMenuBar(myFrame.getMenuBar(this));
		JToolBar tb = myFrame.getToolBar(this);
		tb.setRollover(true);
		tb.setFloatable(false);
		myFrame.add(tb, BorderLayout.PAGE_START);
		myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		myFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				KernelAction.EXIT.getActionFor(GUIManagerAgent.this).actionPerformed(null);
			}
		});
		myFrame.pack();
		myFrame.setVisible(true);
		myFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		myFrame.setResizable(true);
	}

}