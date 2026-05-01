package com.swna.javafx.domain.admin.product;

import lombok.Data;

@Data
public class Product {
   private Long id;
   private String name;
   private int price;

   public Product(long l, String string, int i) {
     this.id = l;
     this.name = string;
     this.price = i;
   }
}