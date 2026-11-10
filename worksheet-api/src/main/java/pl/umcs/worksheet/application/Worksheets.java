package pl.umcs.worksheet.application;

import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.Direction;
import pl.umcs.worksheet.domain.Worksheet;
import pl.umcs.worksheet.domain.WorksheetId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface Worksheets {

	void add(Worksheet worksheet);

	Optional<WorksheetId> openedBy(byte[] keyHash);

	List<DatedRate> ratesOf(WorksheetId worksheet);

	List<DatedRate> ratesIn(WorksheetId worksheet, Collection<Direction> directions);

	void change(WorksheetId worksheet, Consumer<Worksheet> change);
}
