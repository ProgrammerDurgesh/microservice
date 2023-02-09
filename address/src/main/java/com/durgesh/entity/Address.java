package com.durgesh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private String line1;
    private String line2;
    private String State;
    @Column(length = 6)
    private String zip;
   /* @OneToOne(cascade = CascadeType.ALL)
    private Employ*/
}
