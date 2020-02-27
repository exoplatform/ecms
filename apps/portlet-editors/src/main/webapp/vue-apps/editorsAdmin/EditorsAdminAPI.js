// TODO use Editors Admin API here?

export function getMyAllTasks() {
  return fetch('/rest/tasks', {
    method: 'GET',
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error ('Error when getting my all tasks');
    }
  })
}

export function getMyIncomingTasks() {
  return fetch('/rest/tasks?status=incoming&returnSize=true', {
    method: 'GET',
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    } 
    else {
      throw new Error ('Error when getting my incoming tasks');
    }
  })
}

export function getMyOverdueTasks() {
  return fetch('/rest/tasks?status=overdue&returnSize=true', {
    method: 'GET',
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    } 
    else {
      throw new Error ('Error when getting my overdue tasks');
    }
  })
}
export function getUserInformations(userName) {
  return fetch(`/rest/v1/social/users/${userName}`, {
    method: 'GET',
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when getting user informations');
    }
  })
}

export function updateTask(taskId, task) {
  return fetch(`/rest/tasks/${taskId}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'PUT',
    body: JSON.stringify(task)
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when updating task');
    }
  })
}