package entities;

import java.util.List;

public class CommentGroups {
    public List<String> view;
    public List<String> edit;
    public List<String> remove;
    public CommentGroups(){

    }
    public CommentGroups(List<String> view, List<String> edit, List<String> remove){
        this.view = view;
        this.edit = edit;
        this.remove = remove;
    }
}
