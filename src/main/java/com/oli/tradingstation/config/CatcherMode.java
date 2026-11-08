package com.oli.tradingstation.config;

public enum CatcherMode {
  /** The NPC is removed from the world and stored in the catcher. */
  CAPTURE,
  /** The NPC is copied into the catcher, the original stays. */
  CLONE,
  /** Capture by default, clone while sneaking. */
  BOTH,
  /** The NPC type can not be put into the catcher. */
  DISABLED
}
