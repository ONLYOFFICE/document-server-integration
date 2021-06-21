package com.onlyoffice.integration.entities.filemodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onlyoffice.integration.serializer.SerializerFilter;
import com.onlyoffice.integration.entities.Group;

import java.util.List;
import java.util.stream.Collectors;

public class CommentGroup {
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> view;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> edit;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> remove;

    public CommentGroup(List<Group> view, List<Group> edit, List<Group> remove) {
        this.view = view.stream()
                .map(group -> group.getName())
                .collect(Collectors.toList());

        this.edit = edit.stream()
                .map(group -> group.getName())
                .collect(Collectors.toList());

        this.remove = remove.stream()
                .map(group -> group.getName())
                .collect(Collectors.toList());
    }

    public List<String> getView() {
        return view;
    }

    public List<String> getEdit() {
        return edit;
    }

    public List<String> getRemove() {
        return remove;
    }
}
