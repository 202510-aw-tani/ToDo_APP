package com.example.todo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.Todo;

public interface TodoMapper {
    List<Todo> findAllByUserId(@Param("userId") Long userId);

    List<Todo> findPage(@Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sortByPriority") boolean sortByPriority,
            @Param("sortByDeadline") boolean sortByDeadline,
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId);

    long countAll(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    int insert(Todo todo);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIdsAndUserId(@Param("ids") List<Integer> ids, @Param("userId") Long userId);

    Todo findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int existsById(@Param("id") Long id);

    int countByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateByIdAndUserId(Todo todo);

    int assignUnownedTodosToUser(@Param("userId") Long userId);
}
