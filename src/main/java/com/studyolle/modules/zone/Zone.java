package com.studyolle.modules.zone;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter @Setter
@Builder @AllArgsConstructor @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"city", "province"}))
public class Zone {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String localNameOfCity;

    private String province;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Zone zone = (Zone) o;

        return Objects.equals(id, zone.id);
    }

    @Override
    public int hashCode() {
        return 163216639;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)/%s", city, localNameOfCity, province);
    }

}
