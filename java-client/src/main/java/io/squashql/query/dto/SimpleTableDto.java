package io.squashql.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import io.squashql.query.TableUtils;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTableDto {

  public List<String> columns;
  public List<List<Object>> rows;

  public void show() {
    System.out.println(this);
  }

  @Override
  public String toString() {
    return TableUtils.toString(this.columns, this.rows, String::valueOf, String::valueOf);
  }
}
