package org.telekit.plugins.linetest.database;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.telekit.base.domain.security.Credentials;
import org.telekit.base.domain.security.UsernamePasswordCredentials;
import org.telekit.base.net.connection.BaseConnectionParams;
import org.telekit.plugins.linetest.DatabaseTest;
import org.telekit.plugins.linetest.demo.DummyEncryptionService;
import org.telekit.plugins.linetest.domain.*;
import org.telekit.test.util.UUIDHelper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.collections4.CollectionUtils.union;
import static org.assertj.core.api.Assertions.assertThat;
import static org.telekit.base.net.connection.Scheme.SSH;

@DatabaseTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class MeasurementTaskRepositoryTest {

    static final LocalDateTime BASE_DATE = LocalDateTime.of(2010, 4, 25, 14, 50);
    static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    private final MeasurementTaskRepository repository;

    public MeasurementTaskRepositoryTest(DataSource dataSource) {
        repository = new MeasurementTaskRepository(dataSource);
    }

    @Test
    public void getAllReturnsExpectedItems() {
        Collection<MeasurementTask> initialItems = repository.getAll();
        Collection<MeasurementTask> newItems = List.of(
                generateTask(),
                generateTask()
        );
        repository.addAll(newItems);

        Collection<MeasurementTask> result = repository.getAll();
        assertThat(result).containsExactlyInAnyOrderElementsOf(union(initialItems, newItems));
    }

    @Test
    public void getFromEndReturnsExpectedItems() {
        List<MeasurementTask> items = List.of(
                generateTask(),
                generateTask(),
                generateTask(),
                generateTask(),
                generateTask()
        );
        repository.addAll(items);

        Collection<MeasurementTask> result = repository.getFromEnd(0, 3);
        assertThat(result).containsExactly(items.get(4), items.get(3), items.get(2));
    }

    @Test
    public void countAllItemsReturnsExpectedSize() {
        long initialCount = repository.count();
        List<MeasurementTask> newItems = List.of(
                generateTask(),
                generateTask()
        );
        repository.addAll(newItems);

        long resultCount = repository.count();
        assertThat(resultCount).isEqualTo(initialCount + newItems.size());
    }

    @Test
    public void existingTaskCanBeFoundById() {
        MeasurementTask task = generateTask();
        repository.add(task);

        Optional<MeasurementTask> result = repository.findById(task.getId());
        assertThat(result).isNotEmpty();
        assertThat(result).get().usingRecursiveComparison()
                .ignoringFields("startDateTime", "duration", "result")
                .isEqualTo(task);
    }

    @Test
    public void findingTaskByFakeIdReturnsEmptyResult() {
        MeasurementTask task = generateTask();

        assertThat(repository.findById(task.getId())).isNotPresent();
    }

    @Test
    public void taskWithAllFieldsValidCanBeAdded() {
        MeasurementTask task = generateTask();
        repository.add(task);

        Optional<MeasurementTask> result = repository.findById(task.getId());
        assertThat(result).isPresent();
    }

    @Test
    public void taskWithAllFieldsValidAndCanBeAddedWithPasswordProtection() {
        MeasurementTask task = generateTask();
        repository.setEncryptionService(new DummyEncryptionService());

        try {
            repository.add(task);

            Optional<MeasurementTask> result = repository.findById(task.getId());
            assertThat(result).isPresent();
            assertThat(getPasswordFrom(result.get())).isEqualTo(getPasswordFrom(task));
        } finally {
            repository.setEncryptionService(null);
        }
    }

    @Test
    public void taskWithAllFieldsValidCanBeUpdated() {
        MeasurementTask task = generateTask();
        repository.add(task);
        assertThat(repository.findById(task.getId())).isPresent();

        Measurement measurement = generateMeasurementResult();
        task.getRequest().setProvider(task.getProvider() + "_updated");
        task.getRequest().setLine(task.getLine() + "_updated");
        task.setDateTime(BASE_DATE);
        task.setDuration(299);
        task.setResult(measurement);
        repository.update(task);

        Optional<MeasurementTask> result = repository.findById(task.getId());
        assertThat(result).isNotEmpty();
        assertThat(result).get().usingRecursiveComparison().isEqualTo(task);
    }

    @Test
    public void taskWithAllFieldsValidCanBeUpdatedWithPasswordProtection() {
        MeasurementTask task = generateTask();
        repository.add(task);
        assertThat(repository.findById(task.getId())).isPresent();

        MeasurementTask updatedTask = copyWithNewPassword(task, "BRAND_NEW_PASSWORD");
        updatedTask.setDateTime(BASE_DATE);
        repository.setEncryptionService(new DummyEncryptionService());

        try {
            repository.update(updatedTask);

            Optional<MeasurementTask> result = repository.findById(task.getId());
            assertThat(result).isPresent();
            assertThat(result).get().usingRecursiveComparison().isEqualTo(updatedTask);
            assertThat(getPasswordFrom(result.get())).isEqualTo(getPasswordFrom(updatedTask));
        } finally {
            repository.setEncryptionService(null);
        }
    }

    @Test
    public void existingTaskCanBeRemovedById() {
        MeasurementTask task = generateTask();
        assertThat(repository.findById(task.getId())).isEmpty();

        repository.add(task);
        assertThat(repository.findById(task.getId())).isPresent();

        repository.removeById(task.getId());
        assertThat(repository.findById(task.getId())).isEmpty();
    }

    private MeasurementTask generateTask() {
        UUID id = UUIDHelper.fromInt(ID_COUNTER.incrementAndGet());

        LinetestRequest request = new LinetestRequest(
                new BaseConnectionParams(SSH, "192.168.1.1", 22, UsernamePasswordCredentials.of("root", "qwerty")),
                "HuaweiMA5600Provider",
                "1"
        );

        MeasurementTask task = new MeasurementTask(id, request);
        task.setDateTime(BASE_DATE.plusHours(ID_COUNTER.incrementAndGet()));

        return task;
    }

    private Measurement generateMeasurementResult() {
        Measurement measurement = new Measurement();
        measurement.setTestFailed(false);
        measurement.setLineStatus(LineStatus.ON_HOOK);
        measurement.setMeasuredValue(new MeasuredValue(MeasuredValue.ValueType.RESISTANCE, 1, 2, 3));
        measurement.setMeasuredValue(new MeasuredValue(MeasuredValue.ValueType.CAPACITANCE, 1, 2, 3));
        measurement.setMeasuredValue(new MeasuredValue(MeasuredValue.ValueType.AC_VOLTAGE, 1, 2, 3));
        measurement.setMeasuredValue(new MeasuredValue(MeasuredValue.ValueType.DC_VOLTAGE, 1, 2, 3));
        measurement.setRawOutput("RAW_OUTPUT");

        return measurement;
    }

    @SuppressWarnings("SameParameterValue")
    private MeasurementTask copyWithNewPassword(MeasurementTask task, String newPassword) {
        UsernamePasswordCredentials userPassword = Objects.requireNonNull(getCredentialsFrom(task));
        LinetestRequest request = LinetestRequest.of(
                task.getConnectionParams().toUri(),
                UsernamePasswordCredentials.of(userPassword.getUsername(), newPassword),
                task.getProvider(),
                task.getLine()
        );
        return new MeasurementTask(task.getId(), request);
    }

    private @Nullable UsernamePasswordCredentials getCredentialsFrom(MeasurementTask task) {
        Credentials cred = task.getConnectionParams().getCredentials();
        return (cred instanceof UsernamePasswordCredentials userPassword) ? userPassword : null;
    }

    private @Nullable String getPasswordFrom(MeasurementTask task) {
        UsernamePasswordCredentials userPassword = getCredentialsFrom(task);
        return userPassword != null ? userPassword.getPasswordAsString() : null;
    }
}