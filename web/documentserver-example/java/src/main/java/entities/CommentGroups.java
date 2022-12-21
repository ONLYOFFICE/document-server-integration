package entities;

import java.util.List;

public class CommentGroups {
    public List<String> view;
    public List<String> edit;
    public List<String> remove;
    public CommentGroups() {

    }
    public CommentGroups(final List<String> viewParam, final List<String> editParam, final List<String> removeParam) {
        this.view = viewParam;
        this.edit = editParam;
        this.remove = removeParam;
    }
}
