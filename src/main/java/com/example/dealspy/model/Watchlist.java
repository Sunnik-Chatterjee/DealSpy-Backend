package com.example.dealspy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "user_product_watchlist",
        uniqueConstraints = @UniqueConstraint(columnNames = {"uid", "pid"})
)
@NoArgsConstructor
@Getter
@Setter
public class Watchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "uid",referencedColumnName = "uid")
    private User user;

    @ManyToOne
    @JoinColumn(name = "pid", referencedColumnName = "pid")
    private Product product;

    private LocalDate watchEndDate;
}