package com.ankin.rpgmechanics.quest;

/**
 * Result of progressing a quest after a criterion match.
 */
public enum QuestProgressEvent {
    NONE,
    /** First step completed — quest is now active. */
    INITIATED,
    /** A middle step completed. */
    STEP_COMPLETED,
    /** Final step completed — quest finished. */
    COMPLETED
}
