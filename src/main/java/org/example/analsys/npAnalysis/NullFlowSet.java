package org.example.analsys.npAnalysis;

import soot.Local;
import soot.toolkits.scalar.AbstractBoundedFlowSet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 继承自AbstractBoundedFlowSet（它是一个有界流集合）
 * 其实和普通的Set没感觉有太大区别
 */
public class NullFlowSet extends AbstractBoundedFlowSet<Local> {

    private Set<Local> nullLocals = new HashSet<>();
    public NullFlowSet() {
        super();
    }

    @Override
    public NullFlowSet clone() {
        NullFlowSet myClone = new NullFlowSet();
        myClone.nullLocals.addAll(this.nullLocals);
        return myClone;
    }

    @Override
    public boolean isEmpty() {
        return nullLocals.isEmpty();
    }

    @Override
    public int size() {
        return nullLocals.size();
    }

    @Override
    public void add(Local local) {
        nullLocals.add(local);
    }

    @Override
    public void remove(Local local) {
        if(nullLocals.contains(local))
            nullLocals.remove(local);
    }

    @Override
    public boolean contains(Local local) {
        return nullLocals.contains(local);
    }

    @Override
    public Iterator<Local> iterator() {
        return nullLocals.iterator();
    }

    @Override
    public List<Local> toList() {
        return nullLocals.stream().collect(Collectors.toList());
    }
}
