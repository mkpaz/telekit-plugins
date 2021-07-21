package org.telekit.plugins.linetest.tool;

import org.telekit.plugins.linetest.database.CsvPhoneBook;
import org.telekit.plugins.linetest.database.PhoneBook;
import org.telekit.plugins.linetest.domain.PhoneBookEntry;

import java.util.ArrayList;
import java.util.List;

public class PhoneBookService {

    private final List<PhoneBook> phoneBooks = new ArrayList<>();

    public PhoneBookService() {
        phoneBooks.add(new CsvPhoneBook());
    }

    public List<PhoneBookEntry> find(String pattern, int limit) {
        List<PhoneBookEntry> result = new ArrayList<>();
        for (PhoneBook phoneBook : phoneBooks) {
            result.addAll(phoneBook.find(pattern, limit));
        }
        return result;
    }
}
