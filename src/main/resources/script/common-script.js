export const viewDelay = 500;

export function wait(ms) {
    const start = new Date().getTime();
    let end = start;
    while (end < start + ms) {
        end = new Date().getTime();
    }
}

export function getTodoList() {
    return fetch('data/todolist.json')
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
