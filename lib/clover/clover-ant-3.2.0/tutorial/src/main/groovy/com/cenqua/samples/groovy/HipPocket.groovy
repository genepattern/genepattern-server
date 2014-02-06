package com.cenqua.samples.groovy;


import com.cenqua.samples.money.IMoney;
import com.cenqua.samples.money.Money;

class HipPocket {


  Map<String, IMoney> pocket = [:]

  def insert(int amt, String currency) {
    final IMoney existingMoney = pocket[currency]
    final Money newMoney = new Money(amt, currency)
    if (existingMoney) {
      pocket[currency] = existingMoney.add(newMoney);
    } else {
      pocket[currency] = newMoney;
    }
  }

  def boolean isEmpty() {
    // find any non-zero entries in the map.
    return pocket.isEmpty() || pocket.findAll{ !it.value.isZero() }.isEmpty()

  }

}

