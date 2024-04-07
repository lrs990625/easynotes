package com.lrs.domian;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelWord implements Serializable {

    private String word;
    private String pronounces;
    private List<String> meanings;
}
