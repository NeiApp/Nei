package com.projectii.Nei.ui.model;

import java.util.List;

/**
 * Shape of a week or month review. Your Spring AI endpoint can return this as structured output.
 * Compute the numbers in code (done, skipped, total, streak, best, counts) and ask the model only
 * for summary, wins, watch and tryNext.
 */
public record ReviewData(String title, String range, int done, int skipped, int total, int streak, String best,
                         String[] labels, int[] counts, String summary,
                         List<String> wins, List<String> watch, String tryNext) {}
