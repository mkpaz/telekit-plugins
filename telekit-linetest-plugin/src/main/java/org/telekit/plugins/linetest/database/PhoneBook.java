package org.telekit.plugins.linetest.database;

import org.telekit.plugins.linetest.domain.PhoneBookEntry;

import java.util.List;

public interface PhoneBook {

    List<PhoneBookEntry> find(String pattern, int limit);
}
