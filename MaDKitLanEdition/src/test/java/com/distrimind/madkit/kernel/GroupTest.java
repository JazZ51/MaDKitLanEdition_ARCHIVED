/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import com.distrimind.madkit.util.SerializationTools;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class GroupTest {

	@Test
	public void testAll() throws ClassNotFoundException, IOException
	{
		for (Object[] o : provideCompleteGroupArgumentsList())
		{
			String s[]=new String[o.length-4];
			for (int i=4;i<o.length;i++)
				s[i-4]=(String)o[i];
			testGroupConstructorDistributed((Boolean)o[0], (Gatekeeper)o[1], (Boolean)o[2], (String)o[3], s );
		}
		for (Object[] o : provideGroupList())
		{
			String s[]=new String[o.length-1];
			for (int i=1;i<o.length;i++)
				s[i-1]=(String)o[i];
			testGroupConstructor((String)o[0], s );
		}
		for (Object[] o : provideCompleteGroupArgumentsList())
		{
			String s[]=new String[o.length-4];
			for (int i=4;i<o.length;i++)
				s[i-4]=(String)o[i];
			testCloneGroup((Boolean)o[0], (Gatekeeper)o[1], (Boolean)o[2], (String)o[3], s );
		}
		for (Object[] o : provideCompleteGroupArgumentsList())
		{
			String s[]=new String[o.length-4];
			for (int i=4;i<o.length;i++)
				s[i-4]=(String)o[i];
			testGroupSerialization((Boolean)o[0], (Gatekeeper)o[1], (Boolean)o[2], (String)o[3], s );
		}
		
		testGroupsOperator();
	}
	
	public void testGroupConstructor(String community, String... groups) {
		Group g = new Group(community, groups);
		Assert.assertEquals(g.getCommunity(), community);

		for (int i = 0; i < groups.length; i++) {
			Assert.assertEquals(g.getName(), groups[groups.length - i - 1]);
			g = g.getParent();
		}
		new Group(community, groups);
	}

	public void testGroupConstructorDistributed(boolean isDistributed, Gatekeeper gateKeeper, boolean _isReserved,
			String community, String... groups) {
		Group g = new Group(isDistributed, gateKeeper, _isReserved, community, groups);
		Assert.assertEquals(g.getCommunity(), community);
		Assert.assertEquals(g.getGateKeeper(), gateKeeper);

		String gpath = g.getPath();

		if (groups.length == 1 && groups[0].contains("/")) {
			groups = groups[0].split("/");
		}
		StringBuilder path = new StringBuilder("/");
		for (int i = groups.length - 1; i >= 0; i--) {
			Assert.assertEquals(g.getName(), groups[i]);
			path.append(groups[groups.length - i - 1]).append("/");
			Group g2 = g;
			g = g.getParent();

			Assert.assertEquals(g.getSubGroup(g2.getName()), g2);
		}
		Assert.assertEquals(gpath, path.toString());
		for (String group : groups) {
			g = g.getSubGroup(group);
			Assert.assertEquals(g.getName(), group);
		}

		try {
			new Group(community, groups);
			if (_isReserved)
				Assert.fail();
		} catch (Exception e) {
			if (!_isReserved)
				throw e;
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testGroupReserved() {
		try {
			Group g = new Group(true, null, true, "CR", "GR");
			Group g2 = new Group("CR", "GR");
			Assert.fail();
		} catch (IllegalArgumentException ignored) {

		}

	}

	public void testGroupsOperator() {
		Group g = new Group("C1", "G2");
		Assert.assertTrue(AbstractGroup.getUniverse().includes(g));
		Assert.assertTrue(new Group("C1", "G2").includes(g));
		Assert.assertFalse(g.includes(new Group("C1", "G2", "G3")));
		g = new Group(true, "C1", "G2");
		Assert.assertTrue(AbstractGroup.getUniverse().includes(g));
		Assert.assertFalse(new Group("C1", "G2").includes(g));
		Assert.assertTrue(g.includes(new Group("C1", "G2")));
		Assert.assertTrue(g.includes(new Group("C1", "G2", "G3")));

	}

	public void testCloneGroup(boolean isDistributed, Gatekeeper gateKeeper, boolean _isReserved, String community,
			String... groups) {
		Group g = new Group(isDistributed, gateKeeper, _isReserved, community, groups);
		Group g2 = g.clone();
		Assert.assertEquals(g, g2);
		if (groups.length == 1 && groups[0].contains("/")) {
			groups = groups[0].split("/");
		}
		for (int i = groups.length - 1; i >= 0; i--) {
			Assert.assertEquals(g2.getName(), groups[i]);
			g2 = g2.getParent();
		}
	}

	public void testGroupSerialization(boolean isDistributed, Gatekeeper gateKeeper, boolean _isReserved,
			String community, String... groups) throws IOException, ClassNotFoundException {
		Group g = new Group(isDistributed, gateKeeper, _isReserved, community, groups);
		System.out.println("Test group serialization : " + g);
		byte[] array;

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				SerializationTools.writeExternalizableAndSizable(oos, g, false);
			}
			array = baos.toByteArray();
		}

		Group g2;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(array)) {
			try (ObjectInputStream ois = new ObjectInputStream(bais)) {
				g2 = (Group) SerializationTools.readExternalizableAndSizable(ois, false);
			}
		}

		Assert.assertEquals(g, g2);
		if (groups.length == 1 && groups[0].contains("/")) {
			groups = groups[0].split("/");
		}
		for (int i = groups.length - 1; i >= 0; i--) {
			Assert.assertEquals(g2.getName(), groups[i]);
			g2 = g2.getParent();
		}
	}

	@Test
	public void testNullEmptyAndInvalidArguments() {
		try {
			new Group(null, "GE1");
			Assert.fail();
		} catch (NullPointerException ignored) {

		}
		try {
			new Group("", "GE1");
			Assert.fail();
		} catch (IllegalArgumentException ignored) {

		}

		try {
			new Group("CE1");
		} catch (Exception e) {
			Assert.fail();
		}
		try {
			new Group("CE1", "GE1", null, "GE3");
			Assert.fail();
		} catch (NullPointerException ignored) {

		}
		try {
			new Group("CE1", "GE1", "", "GE3");
			Assert.fail();
		} catch (IllegalArgumentException ignored) {

		}
		try {
			new Group("CE1", "GE1", "sf;gs", "GE3");
			Assert.fail();
		} catch (IllegalArgumentException ignored) {

		}
		try {
			new Group("CE1", "sf;gs", "GE3");
			Assert.fail();
		} catch (IllegalArgumentException ignored) {

		}
	}

	public static Object[][] provideGroupList() {
		return new Object[][] { { "C1", "G1" }, { "C1", "G1", "G2" }, { "C1", "G2", "G3" }, { "C2", "G4" } };
	}

	public static Object[][] mixArguments(Object[][] args1, Object[][] args2) {
		Object[][] res = new Object[args2.length * args1.length][];
		int index = 0;
		for (Object[] anArgs2 : args2) {
			for (Object[] anArgs1 : args1) {
				Object v[] = new Object[anArgs2.length + anArgs1.length];
				int iv = 0;
				for (int k = 0; k < anArgs1.length; k++)
					v[iv++] = anArgs1[k];

				for (int k = 0; k < anArgs2.length; k++)
					v[iv++] = anArgs2[k];

				res[index++] = v;
			}
		}
		return res;

	}

	public static Object[][] provideCompleteGroupArgumentsList() {
		return mixArguments(provideGroupParameters(),
				concatArgs(provideGroupList(), transformAGroupSetToOneString(provideGroupList())));
	}

	public static Object[][] concatArgs(Object[][] arg1, Object[][] arg2) {
		Object[][] res = new Object[arg1.length + arg2.length][];
		int index = 0;
		for (Object[] anArg1 : arg1) res[index++] = anArg1;
		for (Object[] anArg2 : arg2) res[index++] = anArg2;
		return res;
	}

	public static Object[][] transformAGroupSetToOneString(Object[][] arg) {
		Object res[][] = new Object[arg.length][];
		for (int i = 0; i < res.length; i++) {
			res[i] = new Object[2];
			res[i][0] = arg[i][0];
			StringBuilder g = null;
			for (int k = 1; k < arg[i].length; k++) {
				if (g == null)
					g = new StringBuilder(arg[i][k].toString());
				else
					g.append("/").append(arg[i][k]);
			}
			res[i][1] = g.toString();
		}
		return res;
	}

	public static Object[][] provideGroupParameters() {
		Gatekeeper gk1 = new Gatekeeper() {

			@Override
			public boolean allowAgentToTakeRole(Group _group, String _roleName,
					final Class<? extends AbstractAgent> requesterClass, AgentNetworkID _agentNetworkID,
					Object _memberCard) {
				return false;
			}

			@Override
			public boolean allowAgentToCreateSubGroup(Group _parent_group, Group _sub_group,
					final Class<? extends AbstractAgent> requesterClass, AgentNetworkID _agentNetworkID,
					Object _memberCard) {
				return false;
			}
		};
		return new Object[][] { {Boolean.TRUE, gk1, Boolean.FALSE} };
		/*
		 * Gatekeeper gk2=new Gatekeeper() {
		 * 
		 * @Override public boolean allowAgentToTakeRole(Group _group, AgentNetworkID
		 * _agentNetworkID, String _roleName, Object _memberCard) { return true; }
		 * 
		 * @Override public boolean allowAgentToCreateSubGroup(Group _parent_group,
		 * Group _sub_group, AgentNetworkID _agentNetworkID, Object _memberCard) {
		 * return false; } }; Gatekeeper gk3=new Gatekeeper() {
		 * 
		 * @Override public boolean allowAgentToTakeRole(Group _group, AgentNetworkID
		 * _agentNetworkID, String _roleName, Object _memberCard) { return false; }
		 * 
		 * @Override public boolean allowAgentToCreateSubGroup(Group _parent_group,
		 * Group _sub_group, AgentNetworkID _agentNetworkID, Object _memberCard) {
		 * return true; } }; Gatekeeper gk4=new Gatekeeper() {
		 * 
		 * @Override public boolean allowAgentToTakeRole(Group _group, AgentNetworkID
		 * _agentNetworkID, String _roleName, Object _memberCard) { return true; }
		 * 
		 * @Override public boolean allowAgentToCreateSubGroup(Group _parent_group,
		 * Group _sub_group, AgentNetworkID _agentNetworkID, Object _memberCard) {
		 * return true; } };
		 * 
		 * Object[][] res=new Object[5*2*2][]; int index=0; for (int i=0;i<2;i++) {
		 * Boolean isDistribued=null; if (i==0) isDistribued=new Boolean(true); else
		 * isDistribued=new Boolean(false); for (int j=0;j<2;j++) { Boolean
		 * isReserved=null; if (j==0) isReserved=new Boolean(false); else isReserved=new
		 * Boolean(true); for (int k=0;k<5;k++) { Gatekeeper gk=null; switch(k) { case
		 * 1: gk=gk1; break; case 2: gk=gk2; break; case 3: gk=gk3; break; case 4:
		 * gk=gk4; break; } res[index++]=new Object[]{isDistribued, gk, isReserved}; } }
		 * } return res;
		 */
	}

}
