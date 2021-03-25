package com.soybeany.log.core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/3/25
 */
public class AllKeyContainChecker {

    private final List<String> keys;
    private final Set<String> keysToMatch = new HashSet<>();

    public AllKeyContainChecker(String... keys) {
        this.keys = Arrays.asList(keys);
    }

    public void init() {
        keysToMatch.addAll(keys);
    }

    public boolean match(String content) {
        keysToMatch.removeIf(content::contains);
        return keysToMatch.isEmpty();
    }

}
