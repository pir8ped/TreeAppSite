package com.john.TreeApp.beans.utilBean;

import com.john.TreeApp.beans.Tree;

public class TreeGroup {
    public TreeGroup(Tree tree, int quantity) {
        this.tree = tree;
        this.quantity = quantity;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    private Tree tree;

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    private int quantity;


    public String getLatinName() {
        return tree.getLatinName();
    }


    public String getEnglishName() {
        return tree.getEnglishName();
    }

    public Tree getTree() {
        return tree;
    }


    public int getQuantity() {
        return quantity;
    }
}

