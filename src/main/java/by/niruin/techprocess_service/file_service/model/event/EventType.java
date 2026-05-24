package by.niruin.techprocess_service.file_service.model.event;

public enum EventType {
    FILE_MOVE_TO_PERMANENT_STORAGE("file-topic"),
    FILE_DELETED_EVENT(FILE_MOVE_TO_PERMANENT_STORAGE.getTopicName());

    private final String topicName;

    EventType(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}
