package me.escoffier.timeless.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class Project {

  public String id;
  public String name;
  public boolean is_archived;
  public String parent_id;


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("name", name).toString();
  }

  public boolean isArchived() {
    return is_archived;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Project project = (Project) o;
    return Objects.equal(id, project.id) &&
            Objects.equal(name, project.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, name);
  }
}
