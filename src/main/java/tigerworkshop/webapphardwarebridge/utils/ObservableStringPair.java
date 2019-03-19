package tigerworkshop.webapphardwarebridge.utils;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ObservableStringPair {
    private StringProperty left = new SimpleStringProperty();
    private StringProperty right = new SimpleStringProperty();

    public ObservableStringPair(String left, String right) {
        setLeft(left);
        setRight(right);
    }

    public static ObservableStringPair of(String left, String right) {
        return new ObservableStringPair(left, right);
    }

    public String getLeft() {
        return left.get();
    }

    public void setLeft(String left) {
        this.left.set(left);
    }

    public StringProperty leftProperty() {
        return left;
    }

    public String getRight() {
        return right.get();
    }

    public void setRight(String right) {
        this.right.set(right);
    }

    public StringProperty rightProperty() {
        return right;
    }

    @Override
    public String toString() {
        return "ObservableStringPair{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}


