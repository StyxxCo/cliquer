import React, { Component } from 'react'
import { Button } from 'reactstrap'
import { connect } from 'react-redux'

import '../css/SearchResults.css'

class SearchResults extends Component {
  componentWillReceiveProps = (nextProps) => {
    const { query, category } = nextProps.match.params
    //API call for searching goes here, redux
  }

  renderGroupPreview = (group, i) => {
    //Sprint 2 or 3
  }

  renderUserPreview = (user, i) => {
    //may need to change button function to be a prop later (to allow reusing for group member searching)
    return (
      <div className="user-result" key={i}>
        <div className="left-content">
          <div className="user-icon">ICON</div>
        </div>
        <div className="right-content">
          <div className="right-top-content">
            <div className="user-name">{user.name}</div>
            <div className="user-reputation">{user.reputation}</div>
          </div>
          <div className="right-bottom-content">
            <div className="user-bio">{user.bio ? user.bio : 'This user has no bio.'}</div>
          </div>
        </div>
        <Button color="success" className="request" onClick={() => this.props.sendFriendRequest(user)}>Send Friend Request</Button>
      </div>
    )
  }

  renderResults = (renderResult) => {
    const { results } = this.props
    if(results) {
      return (
        <div className="list-container">
          <ul className="results-list">
            {results.map((result, i) => {
              if(i % 2 === 0) {
               return renderResult(result, i)
              }
            })}
          </ul>
          <ul className="results-list">
            {results.map((result, i) => {
              if(i % 2 === 1) {
                return renderResult(result, i)
              } 
            })}
          </ul>
        </div>
      )
    }
  }

  render() {
    const { category } = this.props.match.params
    return (
      <div className="SearchResults">
        { category === 'group'
          ? this.renderResults(this.renderGroupPreview) 
          : this.renderResults(this.renderUserPreview)
        }
      </div>
    )
  }
}

const mapStateToProps = (state) => {
	return {
    /*results*/
	}
}

export default connect(mapStateToProps)(SearchResults)