package me.escoffier.timeless.model;

public class Label {

    public String name;
    public long id;

    public Label() {
        // Used by jsonb
    }

    public String getShortName() {
        // iterative to find the first uppercase
        int index = 0;
        boolean found = false;
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                found = true;
                break;
            }
            index = index + 1;
        }
        if (found) {
            return name.substring(index);
        } else {
            return name;
        }

    }

    @Override
    public String toString() {
        return name;
    }

}
