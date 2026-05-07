package com.swna.javafx.admin.product;

import lombok.Data;

@Data
public class Product {
   private Long id;
   private String description;
   private String barcode;
   private int price;

   public Product(long l, String string, int i) {
     this.id = l;
     this.barcode = string;
     this.price = i;
   }
}