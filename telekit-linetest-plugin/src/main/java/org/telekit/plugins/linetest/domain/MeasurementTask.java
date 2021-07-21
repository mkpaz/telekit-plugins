package org.telekit.plugins.linetest.domain;

import org.jetbrains.annotations.Nullable;
import org.telekit.base.net.connection.ConnectionParams;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class MeasurementTask {

    public static final int CONNECTION_NOT_STARTED = -1;
    public static final int CONNECTION_SUCCESS = 0;
    public static final int CONNECTION_FAILED = 1;  // you can use any positive value as error code
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");

    public enum Status {
        NEW, RUNNING, FINISHED, ERROR
    }

    private UUID id;
    private LocalDateTime dateTime = LocalDateTime.now();
    private int duration = -1;
    private int connectionStatus = CONNECTION_NOT_STARTED;
    private LinetestRequest request;
    private Measurement result;
    private boolean autoRun;

    public MeasurementTask(LinetestRequest request) {
        this(UUID.randomUUID(), request);
    }

    public MeasurementTask(UUID id, LinetestRequest request) {
        this.id = Objects.requireNonNull(id);
        this.request = Objects.requireNonNull(request);
    }

    public MeasurementTask(MeasurementTask that) {
        this.id = that.id;
        this.dateTime = that.dateTime;
        this.duration = that.duration;
        this.connectionStatus = that.connectionStatus;
        this.request = that.request;
        this.result = that.result;
        this.autoRun = that.autoRun;
    }

    public UUID getId() {
        return id;
    }

    private void setId(UUID id) {
        this.id = Objects.requireNonNull(id);
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = Objects.requireNonNull(dateTime);
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public LinetestRequest getRequest() {
        return request;
    }

    public void setRequest(LinetestRequest request) {
        this.request = Objects.requireNonNull(request);
    }

    public @Nullable Measurement getResult() {
        return result;
    }

    public void setResult(@Nullable Measurement result) {
        this.result = result;
    }

    public boolean isAutoRun() {
        return autoRun;
    }

    public void setAutoRun(boolean autoRun) {
        this.autoRun = autoRun;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MeasurementTask that = (MeasurementTask) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MeasurementTask{" +
                "id=" + id +
                ", dateTime=" + dateTime +
                ", duration=" + duration +
                ", request=" + request +
                ", result=" + result +
                ", autoRun=" + autoRun +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getFormattedDateTime() {
        return DATE_FORMATTER.format(dateTime);
    }

    public String getProvider() {
        return request.getProvider();
    }

    public String getLine() {
        return request.getLine();
    }

    public ConnectionParams getConnectionParams() {
        return request.getConnectionParams();
    }

    public @Nullable String getHost() {
        return getConnectionParams().getHost();
    }

    public void setConnectionSucceeded() {
        connectionStatus = CONNECTION_SUCCESS;
    }

    public void setConnectionFailed() {
        connectionStatus = CONNECTION_FAILED;
    }

    public void resetConnectionStatus() {
        connectionStatus = CONNECTION_NOT_STARTED;
    }

    public boolean isConnectionFailed() {
        return connectionStatus > 0;
    }

    public boolean isFailed() {
        return isConnectionFailed() || (result != null && result.isTestFailed());
    }

    public MeasurementTask duplicate() {
        MeasurementTask copy = new MeasurementTask(this);
        copy.setId(UUID.randomUUID());
        copy.setDateTime(LocalDateTime.now());
        copy.setDuration(-1);
        copy.setConnectionStatus(CONNECTION_NOT_STARTED);
        copy.setRequest(new LinetestRequest(request));
        copy.setResult(null);
        return copy;
    }
}
