package org.smssecure.smssecure.database.documents;

import java.util.List;

public interface Document<T> {

    int size();

    List<T> getList();

}
