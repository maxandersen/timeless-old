package me.escoffier.timeless.model;

import java.util.List;

public interface Inbox {


    List<Runnable> getPlan(Backend backend);

}
