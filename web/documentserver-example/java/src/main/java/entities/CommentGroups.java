package entities;

import java.util.List;

public class CommentGroups {
    public List<String> view;
    public List<String> edit;
    public List<String> remove;
    public CommentGroups() {

    }
    public CommentGroups(List<String> viewParam, List<String> editParam, List<String> removeParam) {
        this.view = viewParam;
        this.edit = editParam;
        this.remove = removeParam;
    }
}
