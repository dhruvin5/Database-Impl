package org.example;

import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

public class Caller {
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(10);
        Utilities.loadDataset(bufferManager, "C:/Users/bhaga/Downloads/title.basics.tsv");
    }
}