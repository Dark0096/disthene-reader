package net.iponweb.disthene.reader.handler.response;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = "id")
public class HierarchyMetricPath {
    private int allowChildren;
    private int expandable;
    private String id;
    private int leaf;
    private String text;

    private HierarchyMetricPath() {}

    public static HierarchyMetricPath of(String path, Integer depth, Boolean leaf) {
        Preconditions.checkNotNull(path, "path must be set!");
        Preconditions.checkNotNull(depth, "depth must be set!");
        Preconditions.checkNotNull(leaf, "leaf must be set!");

        HierarchyMetricPath hierarchyMetricPath = new HierarchyMetricPath();
        hierarchyMetricPath.setAllowChildren(leaf ? 0 : 1);
        hierarchyMetricPath.setExpandable(leaf ? 0 : 1);
        hierarchyMetricPath.setLeaf(leaf ? 0 : 1);
        hierarchyMetricPath.setId(path);
        hierarchyMetricPath.setText(path.substring(path.lastIndexOf('.') + 1));

        return hierarchyMetricPath;
    }

    public int getAllowChildren() {
        return allowChildren;
    }

    public void setAllowChildren(int allowChildren) {
        this.allowChildren = allowChildren;
    }

    public int getExpandable() {
        return expandable;
    }

    public void setExpandable(int expandable) {
        this.expandable = expandable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLeaf() {
        return leaf;
    }

    public void setLeaf(int leaf) {
        this.leaf = leaf;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "HierarchyMetricPath{" +
                "allowChildren=" + allowChildren +
                ", expandable=" + expandable +
                ", id='" + id + '\'' +
                ", leaf=" + leaf +
                ", text='" + text + '\'' +
                '}';
    }
}
