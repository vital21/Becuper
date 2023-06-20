package com.example.alhohelp.entity;

import javax.persistence.*;

@Entity
@Table(name = "general_access")
public class GeneralAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long userId;
    private String address;
    private Long useUser;
    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getUseUser() {
        return useUser;
    }

    public void setUseUser(Long useUser) {
        this.useUser = useUser;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }
}
