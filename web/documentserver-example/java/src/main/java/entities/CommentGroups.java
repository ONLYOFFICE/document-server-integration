package entities;

import java.util.List;

public class CommentGroups {
    private List<String> view;
    private List<String> edit;
    private List<String> remove;
    public CommentGroups() {

    }
    public CommentGroups(final List<String> viewParam, final List<String> editParam, final List<String> removeParam) {
        this.view = viewParam;
        this.edit = editParam;
        this.remove = removeParam;
    }
}
