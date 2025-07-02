package com.example.dealspy.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_product_save_for_later", uniqueConstraints = @UniqueConstraint(columnNames = {"uid", "pid"}))
@NoArgsConstructor
@Getter
@Setter
public class SaveForLater {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "uid",referencedColumnName = "uid")
    private User user;

    @ManyToOne
    @JoinColumn(name = "pid", referencedColumnName = "pid")
    private Product product;

}
