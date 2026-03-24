package com.example.todo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;

@MybatisTest
@MapperScan("com.example.todo.mapper")
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Transactional
@Sql(scripts = "classpath:schema.sql")
class TodoMapperTest {

    @Autowired
    private TodoMapper todoMapper;

    @Test
    @DisplayName("insert/findById: ToDoを登録して取得できる")
    void insertAndFindById_shouldWork() {
        Todo todo = new Todo();
        todo.setTitle("Mapper test insert");
        todo.setCompleted(false);
        todo.setPriority(Priority.MEDIUM);
        todo.setUserId(null);
        todo.setCategoryId(null);
        todo.setDeadline(LocalDate.of(2026, 4, 1));
        todo.setCreatedBy("tester");
        todo.setCreatedAt(LocalDate.of(2026, 3, 24));

        int inserted = todoMapper.insert(todo);
        Todo found = todoMapper.findById(todo.getId());

        assertThat(inserted).isEqualTo(1);
        assertThat(todo.getId()).isNotNull();
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Mapper test insert");
        assertThat(found.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("update/delete: ToDoを更新して削除できる")
    void updateAndDelete_shouldWork() {
        Todo todo = new Todo();
        todo.setTitle("Before update");
        todo.setCompleted(false);
        todo.setPriority(Priority.LOW);
        todo.setUserId(null);
        todo.setCategoryId(null);
        todo.setDeadline(LocalDate.of(2026, 4, 2));
        todo.setCreatedBy("tester");
        todo.setCreatedAt(LocalDate.of(2026, 3, 24));
        todoMapper.insert(todo);

        todo.setTitle("After update");
        todo.setPriority(Priority.HIGH);
        int updated = todoMapper.updateById(todo);
        Todo updatedTodo = todoMapper.findById(todo.getId());

        int deleted = todoMapper.deleteById(todo.getId());
        Todo deletedTodo = todoMapper.findById(todo.getId());

        assertThat(updated).isEqualTo(1);
        assertThat(updatedTodo).isNotNull();
        assertThat(updatedTodo.getTitle()).isEqualTo("After update");
        assertThat(updatedTodo.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(deleted).isEqualTo(1);
        assertThat(deletedTodo).isNull();
    }
}
