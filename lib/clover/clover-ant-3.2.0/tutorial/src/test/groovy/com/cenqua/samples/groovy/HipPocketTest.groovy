package com.cenqua.samples.groovy;

import groovy.util.GroovyTestCase;

class HipPocketTest extends GroovyTestCase {

  public void testEmptyOnCreate() {
    HipPocket pocket = new HipPocket()
    assertTrue pocket.isEmpty()
  }

  public void testInsert() {
    HipPocket pocket = new HipPocket()
    assertTrue pocket.isEmpty()
    pocket.insert(2, "EUR")
    assertTrue !pocket.isEmpty()
  }
}

