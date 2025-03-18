import {getTodoList, viewDelay} from '/script/common-script.js';
import React from 'https://esm.sh/react@18/?dev';
import ReactDOM from 'https://esm.sh/react-dom@18/client?dev';

// Define a component called Greetings
function Greetings() {
    const [isLoading, setIsLoading] = React.useState(true);
    const [todoList, setTodoList] = React.useState([]);

    React.useEffect(() => {
        async function fetchTodoList() {
            setTimeout(async () => {
                const list = await getTodoList();
                setTodoList(list);
                setIsLoading(false);
            }, viewDelay);
        }

        fetchTodoList();
    }, []);

    return <div>
        <h1>Todo List</h1>
        <ul>
            {isLoading ? <p>Loading...</p> : todoList.map((item, index) => {
                return <li key={index}>{item.title}</li>
            })}
        </ul>
        <p>It is now {new Date().toLocaleString()}</p>
    </div>;
}

ReactDOM.createRoot(document.getElementById("react-content"))
    .render(<Greetings/>);