import React, { Component } from 'react'
import { connect } from 'react-redux'
import { Alert } from 'reactstrap'
import Websocket from 'react-websocket'

import '../../css/Chat.css'
import { getChatLog, postChatMessage, updateChatLog } from '../../redux/actions'

class Chat extends Component {

  componentWillReceiveProps = (nextProps) => {
    if(nextProps.user && nextProps.token && nextProps.user.uid && !nextProps.group) {
      //TODO: Add URL to link up with backend
      //TODO: prevent unnecessary calling
      this.props.getLog(``, { 'X-Authorization-Firebase': nextProps.token})
    }
  }

  handleMessage = (data) => {
    if(data) {
      const message = data.message
    }
  }

  handleSubmit = (ev) => {
    ev.preventDefault()
    //TODO: verify message object structure, add URL to postmessage
    const message = { message: ev.target.message, owner: this.props.user.uid }
    this.props.updateLog(message)
    this.props.postMessage(``, { 'X-Authorization-Firebase': nextProps.token})
  }

  render() {
    return (
      <div className="Chat">
        <div>
          <Alert color="danger">
          Group chat system is currently under construction. Check back in sprint 2!
          </Alert>
        </div>
        {/*TODO: link up websockets with backend*/}
        <Websocket url="http://example.com"
              onMessage={this.handleMessage}/>
      </div>
    )
  }
}

const mapStateToProps = (state) => {
	return {
    user: state.user.data,
    token: state.auth.token,
	}
}

const mapDispatchToProps = (dispatch) => {
	return {
    getLog: (url, header) => dispatch(getChatLog(url, header)),
    postMessage: (url, header) => dispatch(postChatMessage(url, header)),
    updateLog: (message) => dispatch(updateChatLog(message)),
	}
}


export default connect(mapStateToProps, mapDispatchToProps)(Groups)
