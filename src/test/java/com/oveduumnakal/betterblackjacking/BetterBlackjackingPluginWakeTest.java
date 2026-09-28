/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oveduumnakal.betterblackjacking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.OverlayManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the wake-up animation through {@link BetterBlackjackingPlugin}: the wake hook delegates to
 * {@link WakeAnimationController}, and with the real controller the animation replaces the target's
 * {@code HUMAN_READY} only for the tracked target within the wake window. Like the client, an NPC mock
 * fires {@code AnimationChanged} whenever its animation is set, so the tests also show the replacement
 * doesn't recurse. The player stands in Pollnivneach with a blackjack and Thieving 70, and the config
 * answers with its defaults unless a test changes that.
 */
public class BetterBlackjackingPluginWakeTest
{
	private static final int BASE_X = 3328;

	private static final int BASE_Y = 2944;

	private static final int TILE = 128;

	private static final int TOP_LEVEL = -1;

	private static final String KNOCKOUT = "You smack the bandit over the head and render them unconscious.";

	@Mock
	private Client client;

	@Mock
	private ClientThread clientThread;

	@Mock
	private OverlayManager overlayManager;

	@Mock
	private Player player;

	@Mock
	private WorldView worldView;

	private final BetterBlackjackingConfig config = mock(BetterBlackjackingConfig.class, Answers.CALLS_REAL_METHODS);

	private final List<NPC> scene = new ArrayList<>();

	private final Map<Integer, Integer> animations = new HashMap<>();

	private BetterBlackjackingPlugin plugin;

	private TargetStateView view;

	private int tick = 100;

	private int eventDepth;

	private int deepestEvent;

	private AutoCloseable mocks;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		doAnswer(invocation ->
		{
			invocation.<Runnable>getArgument(0).run();
			return null;
		})
				.when(clientThread)
				.invoke(any(Runnable.class));

		ItemContainer equipment = mock(ItemContainer.class);
		when(equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx()))
				.thenReturn(new Item(ItemID.BLACKJACK_OAK, 1));
		when(client.getTickCount()).thenAnswer(invocation -> tick);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getWorldView(anyInt())).thenReturn(worldView);
		when(client.getTopLevelWorldView()).thenReturn(worldView);
		when(client.getBoostedSkillLevel(Skill.THIEVING)).thenReturn(70);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
		when(worldView.getBaseX()).thenReturn(BASE_X);
		when(worldView.getBaseY()).thenReturn(BASE_Y);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(10 * TILE, 10 * TILE, TOP_LEVEL));

		IndexedObjectSet<NPC> npcs = mock(IndexedObjectSet.class);
		when(npcs.iterator()).thenAnswer(invocation -> new ArrayList<>(scene).iterator());
		doReturn(npcs).when(worldView)
				.npcs();
	}

	@After
	public void tearDown() throws Exception
	{
		mocks.close();
	}

	@Test
	public void wakeHookDelegatesToTheController()
	{
		WakeAnimationController controller = mock(WakeAnimationController.class);
		inject(controller);
		NPC bandit = npc(1, NpcID.FEUD_ARABIAN_GUARD2_1);

		plugin.onTargetWoke(bandit);

		verify(controller).play(bandit);
	}

	@Test
	public void wakePlaysMaxGetUpOnceWithoutRecursing()
	{
		inject(null);
		NPC bandit = startWithBandit();
		knockOut(bandit);
		for (int i = 0; i < 5; i++)
			gameTick();

		animate(bandit, AnimationID.HUMAN_READY);

		verify(bandit, times(1)).setAnimation(anyInt());
		verify(bandit).setAnimation(AnimationID.MAX_GET_UP);
		verify(bandit).setAnimationFrame(0);
		assertEquals(AnimationID.MAX_GET_UP, bandit.getAnimation());
		assertEquals(2, deepestEvent);
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void offLeavesTheGamesSnapToStanding()
	{
		when(config.wakeAnimation()).thenReturn(WakeAnimation.OFF);
		inject(null);
		NPC bandit = startWithBandit();
		knockOut(bandit);
		for (int i = 0; i < 5; i++)
			gameTick();

		animate(bandit, AnimationID.HUMAN_READY);

		verify(bandit, never()).setAnimation(anyInt());
		verify(bandit, never()).setAnimationFrame(anyInt());
		assertEquals(AnimationID.HUMAN_READY, bandit.getAnimation());
		assertEquals(TargetState.KNOCK_OUT, view.stateOf(bandit));
	}

	@Test
	public void onlyTheTrackedTargetWithinTheWindowIsAnimated()
	{
		inject(null);
		NPC bandit = startWithBandit();
		NPC other = npc(2, NpcID.FEUD_ARABIAN_GUARD2_2);
		plugin.onNpcSpawned(new NpcSpawned(other));
		knockOut(bandit);
		animate(bandit, AnimationID.HUMAN_UNCONSCIOUS);
		for (int i = 0; i < 5; i++)
			gameTick();

		animate(other, AnimationID.HUMAN_READY);
		for (int i = 0; i < 3; i++)
			gameTick();

		animate(bandit, AnimationID.HUMAN_READY);

		verify(other, never()).setAnimation(anyInt());
		verify(bandit, never()).setAnimation(anyInt());
	}

	private void inject(WakeAnimationController controller)
	{
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toInstance(client);
			binder.bind(ClientThread.class).toInstance(clientThread);
			binder.bind(OverlayManager.class).toInstance(overlayManager);
			binder.bind(BetterBlackjackingConfig.class).toInstance(config);
			if (controller != null)
				binder.bind(WakeAnimationController.class).toInstance(controller);

			new BetterBlackjackingPlugin().configure(binder);
		});
		plugin = injector.getInstance(BetterBlackjackingPlugin.class);
		view = injector.getInstance(TargetStateView.class);
	}

	private NPC startWithBandit()
	{
		NPC bandit = npc(1, NpcID.FEUD_ARABIAN_GUARD2_1);
		scene.add(bandit);
		plugin.startUp();
		gameTick();
		return bandit;
	}

	private void knockOut(NPC target)
	{
		when(player.getInteracting()).thenReturn(target);
		plugin.onChatMessage(new ChatMessage(null, ChatMessageType.SPAM, "", KNOCKOUT, "", 0));
	}

	private void gameTick()
	{
		tick++;
		plugin.onGameTick(new GameTick());
	}

	private NPC npc(int index, int id)
	{
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(index);
		when(npc.getId()).thenReturn(id);
		when(npc.getName()).thenReturn("Bandit");
		when(npc.getAnimation()).thenAnswer(invocation -> animations.getOrDefault(index, AnimationID.HUMAN_READY));
		doAnswer(invocation ->
		{
			animate(npc, invocation.getArgument(0));
			return null;
		})
				.when(npc)
				.setAnimation(anyInt());
		return npc;
	}

	private void animate(NPC npc, int animationId)
	{
		animations.put(npc.getIndex(), animationId);
		eventDepth++;
		deepestEvent = Math.max(deepestEvent, eventDepth);
		try
		{
			AnimationChanged event = new AnimationChanged();
			event.setActor(npc);
			plugin.onAnimationChanged(event);
		}
		finally
		{
			eventDepth--;
		}
	}
}
