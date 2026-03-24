package com.sap.documentssystem.exceptions;

public class UserAlreadyInActiveException  extends IllegalArgumentException{
    public  UserAlreadyInActiveException() {
        super("User Already Inactive");
    }
}