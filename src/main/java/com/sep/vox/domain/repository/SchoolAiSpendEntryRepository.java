package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.school.SchoolAiSpendEntry;

/**
 * An append-only ledger: only {@code save}, no update, no delete.
 *
 * <p>The READ path does not go through here but through {@code SchoolAiCostQueryRepository} — every screen asks for
 * grouped figures (by day, by person) and never for individual rows, so loading rows into Java and
 * summing there would pull tens of thousands of rows to print thirty numbers.
 */
public interface SchoolAiSpendEntryRepository {

    SchoolAiSpendEntry save(SchoolAiSpendEntry entry);
}
