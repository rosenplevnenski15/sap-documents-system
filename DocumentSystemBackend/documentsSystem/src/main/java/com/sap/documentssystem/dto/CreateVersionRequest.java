package com.sap.documentssystem.dto;


import lombok.Data;


@Data
public class CreateVersionRequest {



        private String fileName;

        private String s3Url;


}