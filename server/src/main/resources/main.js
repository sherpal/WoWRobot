const pre = document.createElement("pre")
document.body.appendChild(pre)

const ws = new WebSocket("ws://" + document.location.host + "/ws/follow-game-state")

ws.onmessage = (event) => {
  console.log(event.data)

  const jsonData = JSON.parse(event.data)

  pre.innerText = JSON.stringify(jsonData, null, 2)
}
