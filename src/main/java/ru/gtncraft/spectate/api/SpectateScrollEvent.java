package ru.gtncraft.spectate.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class SpectateScrollEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private Player scroller;
    private List<Player> scrollList;
	private ScrollDirection direction;

	public SpectateScrollEvent(Player scroller, List<Player> scrollList, ScrollDirection direction) {
		this.scroller = scroller;
		this.scrollList = scrollList;
		this.direction = direction;
	}

	public Player getPlayer() {
		return scroller;
	}

	public List<Player> getSpectateList() {
		return scrollList;
	}

	public ScrollDirection getDirection() {
		return direction;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
