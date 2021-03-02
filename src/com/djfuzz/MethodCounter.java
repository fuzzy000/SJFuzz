package com.djfuzz;

public class MethodCounter implements Comparable {
    public MethodCounter(String signature, int count) {
        this.signature = signature;
        this.count = count;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    private String signature;
    private Integer count;

    @Override
    public int compareTo(Object o) {
        return this.getCount().compareTo(((MethodCounter)o).getCount());
    }
}
