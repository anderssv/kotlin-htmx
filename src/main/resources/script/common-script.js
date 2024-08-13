export const viewDelay = 1000;

export function getTodoList() {
    return fetch('/data/todolist.json')
        .then(response => response.text())
        .then(text => JSON.parse(text))
        .then(todoList => {
            if (todoList) {
                return todoList;
            } else {
                return [];
            }
        });
}
